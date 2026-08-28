import { useAuth } from '../../context/AuthContext';
import { ClientHome } from './_client-home';
import { BarberHome } from './_barber-home';

export default function HomeScreen() {
  const { session } = useAuth();

  if (!session) return null;

  return session.role === 'CLIENT' ? <ClientHome /> : <BarberHome />;
}
